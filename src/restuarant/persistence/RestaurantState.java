
package restuarant.persistence;

import java.io.Serializable;

import restuarant.model.*;
import restuarant.service.*;

public class RestaurantState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MenuService menuService;
    private final OrderServices orderService;
    private final BillingService billingService;

    private final Chef[] chefs;
    private final Manager manager;
    private final Cashier cashier;

    public RestaurantState( MenuService menuService, OrderServices orderService, BillingService billingService, Chef[] chefs, Manager manager, Cashier cashier) {

        if (menuService == null || orderService == null || billingService == null || chefs == null || manager == null || cashier == null) {
             throw new IllegalArgumentException( "Restaurant state is incomplete");
        }

        this.menuService = menuService;
        this.orderService = orderService;
        this.billingService = billingService;
        this.chefs = chefs.clone();
        this.manager = manager;
        this.cashier = cashier;
    }

    public MenuService getMenuService() {
        return menuService;
    }

    public OrderServices getOrderService() {
        return orderService;
    }

    public BillingService getBillingService() {
        return billingService;
    }

    public Chef[] getChefs() {
        return chefs.clone();
    }

    public Manager getManager() {
        return manager;
    }

    public Cashier getCashier() {
        return cashier;
    }
}
