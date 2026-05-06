package model;

public class HealthArticle {
    private String title;
    private String categories;
    private String accessibleUrl;
    private String imageUrl;

    public HealthArticle(String title, String categories, String accessibleUrl, String imageUrl) {
        this.title = title;
        this.categories = categories;
        this.accessibleUrl = accessibleUrl;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getCategories() { return categories; }
    public String getAccessibleUrl() { return accessibleUrl; }
    public String getImageUrl() { return imageUrl; }
}
