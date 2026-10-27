package restuarant.model;

import java.io.Serializable;

public class Menuitem implements Serializable{
    private static  final long serialVersionUID =1L;
    private final int id;
    private String name;
    private String category;
    private double price;
    private java.math.BigDecimal exactPrice;
    public java.math.BigDecimal priceMoney() { return exactPrice==null ? java.math.BigDecimal.valueOf(price).setScale(2,java.math.RoundingMode.HALF_UP) : exactPrice; }
    public void setPrice(java.math.BigDecimal value) {
        if(value==null || value.signum()<0 || value.scale()>2 || !Double.isFinite(value.doubleValue())) throw new IllegalArgumentException("Use a nonnegative price with at most 2 decimals");
        exactPrice=value.setScale(2); price=value.doubleValue();
    }
    private int preparationTime;
    private boolean available;

    public Menuitem(int id,String name,String category,double price,int preparationTime){
        if( id <= 0){
            throw new IllegalArgumentException("Invalid item id");
        }
        this.id = id;
        setName(name);
        setCategory(category);
        setPrice(price);
        setPreparationTime(preparationTime);
        this.available = true;
    }
    public int getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getCategory(){
        return category;
    }
    public double getPrice(){
        return price;
    }
    public int getPreparationTime(){
        return preparationTime;
    }
    public boolean isAvailable(){
        return available;
    }
    public void setName(String name){
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Name required");
        }
        this.name = name;
    }
    public void setCategory(String category){
        if(category == null || category.trim().isEmpty() ){
            throw new IllegalArgumentException("Category required");
        }
        this.category = category;
    }
    public void setPrice(double price){
        if(!Double.isFinite(price) || price<0){
            throw new IllegalArgumentException("Invalid price");
        }
        this.price = price;
        exactPrice=java.math.BigDecimal.valueOf(price).setScale(2,java.math.RoundingMode.HALF_UP);
    }
    public void setPreparationTime(int minutes){
        if(minutes<= 0 ){
            throw new IllegalArgumentException("Preparation time must be positive");
        }
        this.preparationTime = minutes;
    }
    public void setAvailable(boolean available){
        this.available = available;
    }
    public String toString(){
        return String.format("%d | %s | %s | Rs. %s | %d min | %s", id, name, category, priceMoney().toPlainString(), preparationTime, available ? "Available" : "Unavailable");
    }
}