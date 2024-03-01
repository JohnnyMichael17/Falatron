package com.falatron;

//------ Objeto dos Models do site ------//
public class Models {
    private String author;
    private String category;
    private String description;
    private String dublador;
    private String image;
    private String name;
    private String path;

    public Models(String author, String category, String description, String dublador, String image, String name, String path) {
        this.author = author;
        this.category = category;
        this.description = description;
        this.dublador = dublador;
        this.image = image;
        this.name = name;
        this.path = path;
    }

    public Models() {
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getDublador() {
        return dublador;
    }

    public String getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return name;
    }

    public Models setAuthor(String author) {
        this.author = author;
        return this;
    }

    public Models setCategory(String category) {
        this.category = category;
        return this;
    }

    public Models setDescription(String description) {
        this.description = description;
        return this;
    }

    public Models setDublador(String dublador) {
        this.dublador = dublador;
        return this;
    }

    public Models setImage(String image) {
        this.image = image;
        return this;
    }

    public Models setName(String name) {
        this.name = name;
        return this;
    }

    public Models setPath(String path) {
        this.path = path;
        return this;
    }
}