package compilador.model;

import java.util.*;

public class Grammar {

    public static final String END_MARKER = "$";
    List<String> terminals;
    Map<String, NonTerminal> nonTerminals;
    Map<NonTerminal, List<Production>> productions;
    NonTerminal startSymbol;

    public Grammar(){
        terminals = new ArrayList<>();
        nonTerminals = new HashMap<>();
        productions = new HashMap<>();
    }

    public boolean isSymbolTerminal(String symbol){
        return this.terminals.contains(symbol);
    }


    private void addTerminal(String terminal){
        terminals.add(terminal);
    }

    private void mergeTerminals(Map<String, TokenType> terminals){
        for(String terminal: terminals.keySet()){
            if(!this.terminals.contains(terminal)){
                this.terminals.add(terminal);
            }
        }
    }

    private void addNonTerminal(NonTerminal nonTerminal){
        if(!this.nonTerminals.containsKey(nonTerminal.id)){
            nonTerminals.put(nonTerminal.id, nonTerminal);
        }
    }

    public void addProduction(Production production){
        if(this.startSymbol == null){
            this.startSymbol = production.head;
        }
        if (!this.productions.containsKey(production.head)){
            ArrayList<Production> productions = new ArrayList<>();
            productions.add(production);
            this.productions.put(production.head, productions);
        }else{
            List<Production> productions = this.productions.get(production.head);
            productions.add(production);
        }
    }


    public void generateTerminalsAndNon() {
        for(NonTerminal nonTerminal: productions.keySet()){
            addNonTerminal(nonTerminal);
            for(Production production: this.productions.get(nonTerminal)){
                this.mergeTerminals(production.getTerminals());
            }
        }
    }

    //professora pediu pra ser recursivo, mas não entendi muito bem como ...
    public HashMap<NonTerminal, Set<String>> calculateFirsts() {
        HashMap<NonTerminal, Set<String>> result = new HashMap<>();

        //passar por cada não terminal e ver se todas as suas produções começam com terminais e calcula first delas.
        for(String id: nonTerminals.keySet()) { // itera sobre todos os não terminais da gramática
            NonTerminal nonTerminal =  this.nonTerminals.get(id);
            if(nonTerminal.hasAllFirstAsTerminals()){
                result.put(nonTerminal, nonTerminal.getFirsts(this));
            }
        }

        for(String id: nonTerminals.keySet()) { // itera sobre todos os não terminais da gramática
            NonTerminal nonTerminal =  this.nonTerminals.get(id);
            result.put(nonTerminal, nonTerminal.getFirsts(this));
        }
        return result;
    }

    public HashMap<NonTerminal, Set<String>> calculateFollows(){
        HashMap<NonTerminal, Set<String>> result = new HashMap<>();
        Set<String> followStartSymbol = new HashSet<>();
        followStartSymbol.add(END_MARKER);
        result.put(startSymbol, followStartSymbol);
        this.calculateFollowsWithoutEpsilon(result);

        this.calculateFollowsOfEp(result);
        return result;
    }

    private void calculateFollowsOfEp(HashMap<NonTerminal, Set<String>> result) {
        for (NonTerminal nonTerminal : this.nonTerminals.values()) {
            if (!nonTerminal.isFollowCalculated) {
                for (List<Production> productions : this.productions.values()) {
                    for (Production production : productions) {
                        Set<String> fo = production.getFollowOfHead();
                        if (result.containsKey(nonTerminal)) {
                            fo.addAll(result.get(nonTerminal));
                        }
                        nonTerminal.addToFollow(fo);
                        nonTerminal.isFollowCalculated = true;
                        result.put(nonTerminal, fo);
                    }
                }
            }
        }
    }

    private void calculateFollowsWithoutEpsilon(HashMap<NonTerminal, Set<String>> result){
        for (NonTerminal nonTerminal : this.nonTerminals.values()) {
            if (!nonTerminal.isFollowCalculated) {
                for (List<Production> productions : this.productions.values()) {
                    for (Production production : productions) {
                        if (production.hasNonTerminal(nonTerminal)) {
                            Set<String> fo = production.getFollowOfB(nonTerminal, this);
                            if (result.containsKey(nonTerminal)) {
                                fo.addAll(result.get(nonTerminal));
                            }
                            nonTerminal.addToFollow(fo);
                            if (!production.isBetaEp(nonTerminal)) {
                                nonTerminal.isFollowCalculated = true;
                            }
                            result.put(nonTerminal, fo);
                        }
                    }
                }
            }
        }
    }

    public NonTerminal findNonTerminal(String id) {
        return this.nonTerminals.get(id);
    }

    public ParsingTable createParsingTable(){
        ParsingTable m = new ParsingTable(this);
        for (NonTerminal nonTerminal : this.nonTerminals.values()) { // para todos não terminais
            for (List<Production> productions : this.productions.values()) { // para todas produções
                for(Production production: productions){ // para todas as produções
                    for(String terminal: production.head.getFirsts(this)){
                        m.addProductionToTable(terminal, production.head, production);
                    }
                }
            }
        }
        return m;
    }
}
