package com.example.detectorapp.recyclerview;

public class CardItem {
    private int imageId;
    private String title, barcode;

    public CardItem(){

    }

    public CardItem(String title, String barcode) {
        this.title = title;
        this.barcode = barcode;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
