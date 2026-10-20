package com.nevis.search.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Solution A (default, offline): a curated concept map of wealth-management paperwork.
 * Deterministic, free, fast, easy to unit test.
 *
 * <p>Each group is a set of phrases that mean roughly the same thing to an advisor.
 * If the query contains any phrase in a group, we expand to the whole group.
 * Matching works on token sets, so word order and filler words do not matter.
 */
public final class ConceptExpander implements QueryExpander {

    static final List<List<String>> DEFAULT_GROUPS = List.of(
            List.of( // proof of address
                    "address proof", "proof of address", "proof of residence", "residency proof",
                    "utility bill", "electricity bill", "gas bill", "water bill", "council tax",
                    "tenancy agreement", "rental agreement", "lease agreement", "mortgage statement",
                    "bank statement"),
            List.of( // proof of identity
                    "id proof", "proof of identity", "identity document", "identification",
                    "passport", "driving licence", "driving license", "driver license",
                    "national id", "id card", "birth certificate"),
            List.of( // proof of income
                    "income proof", "proof of income", "payslip", "pay slip", "pay stub",
                    "salary slip", "salary statement", "p60", "employment letter"),
            List.of( // tax
                    "tax return", "tax document", "self assessment", "hmrc", "tax certificate",
                    "p60", "w2", "tax statement"),
            List.of( // source of wealth / funds
                    "source of wealth", "source of funds", "inheritance", "probate",
                    "sale of property", "property sale", "dividend statement", "gift letter"),
            List.of( // investments
                    "investment statement", "portfolio statement", "brokerage statement",
                    "custody statement", "holdings report", "isa statement", "pension statement"),
            List.of( // company ownership
                    "company registration", "certificate of incorporation", "articles of association",
                    "shareholder register", "beneficial ownership", "ubo declaration"),
            List.of( // compliance
                    "kyc", "know your customer", "aml", "anti money laundering", "due diligence",
                    "onboarding checklist", "risk profile", "suitability assessment"));

    private record Phrase(String text, Set<String> tokens) {}

    private final List<List<Phrase>> groups;

    public ConceptExpander() {
        this(DEFAULT_GROUPS);
    }

    public ConceptExpander(List<List<String>> groups) {
        this.groups = groups.stream()
                .map(g -> g.stream().map(p -> new Phrase(p, new HashSet<>(TextUtil.tokenize(p)))).toList())
                .toList();
    }

    @Override
    public List<String> expand(String query) {
        Set<String> qTokens = new HashSet<>(TextUtil.tokenize(query));
        if (qTokens.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (List<Phrase> group : groups) {
            boolean hit = group.stream().anyMatch(p -> !p.tokens().isEmpty() && qTokens.containsAll(p.tokens()));
            if (!hit) continue;
            for (Phrase p : group) {
                if (!p.tokens().equals(qTokens) && !out.contains(p.text())) out.add(p.text());
            }
        }
        return out;
    }
}
