package com.cy.rag;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagKnowledgeDocument {

    private String id;

    private String title;

    private List<String> mealTypes = new ArrayList<>();

    private List<String> keywords = new ArrayList<>();

    private String content;

    private boolean alwaysInclude;
}
