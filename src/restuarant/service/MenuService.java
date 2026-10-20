
package restuarant.service;
import java.io.Serializable;

import restuarant.model.*;

public class MenuService implements Serializable{
    private static final long serialVersionUID = 1L;

    private final Menuitem[] menuItems;
    private int count;

    public MenuService() {
        menuItems = new Menuitem[100];
        count = 0;
    }

    public void addMenuItem(Menuitem item) {
        if (item == null) {
            throw new IllegalArgumentException("Menu item cannot be null");
        }

        if (count >= menuItems.length) {
            throw new IllegalStateException("Menu is full");
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
