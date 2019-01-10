package com.example.detectorapp.recyclerview;

public class CardItem {
    private String title;
    private String codenumber;
    private String imageUrl;
    private int format;

    public CardItem(){}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCodenumber() {
        return codenumber;
    }

    public void setCodenumber(String codenumber) {
        this.codenumber = codenumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public CardItem(String title, String codenumber, String imageUrl, int format) {
        this.title = title;
        this.codenumber = codenumber;
        this.imageUrl = imageUrl;
        this.format = format;
    }
}
