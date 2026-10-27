
package restuarant.service;
import java.io.Serializable;

import restuarant.model.*;

public class MenuService implements Serializable{
    private static final long serialVersionUID = 1L;

    private Menuitem[] menuItems;
    private int count;

    public MenuService() {
        menuItems = new Menuitem[100];
        count = 0;
    }

    public void addMenuItem(Menuitem item) {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN);
        if (item == null) {
            throw new IllegalArgumentException("Menu item cannot be null");
        }

        if (count >= menuItems.length) {
            menuItems=java.util.Arrays.copyOf(menuItems, Math.max(1,menuItems.length*2));
        }

        if (findItem(item.getId()) != null) {
            throw new IllegalArgumentException("Duplicate menu item ID");
        }

        menuItems[count++] = item;
    }

    public Menuitem findItem(int id) {
        for (int i = 0; i < count; i++) {
            if (menuItems[i].getId() == id) {
                return menuItems[i];
            }
        }
        return null;
    }

    public Menuitem[] getItems() { return java.util.Arrays.copyOf(menuItems,count); }
    public void updateItem(int id, String name, String category, java.math.BigDecimal price, int minutes, boolean available) {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN);
        Menuitem item=findItem(id); if(item==null) throw new IllegalArgumentException("Unknown dish");
        Menuitem checked=new Menuitem(id,name,category,price.doubleValue(),minutes); checked.setPrice(price);
        item.setName(name); item.setCategory(category); item.setPrice(price); item.setPreparationTime(minutes); item.setAvailable(available);
    }
    public void displayMenu() {
        System.out.println("\n========== MENU ==========");

        for (int i = 0; i < count; i++) {
            System.out.println(menuItems[i]);
        }

        if (count == 0) {
            System.out.println("No items available.");
        }
    }
}
