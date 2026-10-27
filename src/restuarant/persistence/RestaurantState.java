
package restuarant.persistence;

import java.io.Serializable;

import restuarant.model.*;
import restuarant.service.*;

public class RestaurantState implements Serializable {

    private static final long serialVersionUID = 1L;

    private restuarant.auth.StaffService staff;
    private CancellationService cancellations;
    private BusinessDayService businessDay;
    public BusinessDayService getBusinessDay(){if(businessDay==null)businessDay=new BusinessDayService();return businessDay;}
    private java.util.List<String> history;
    public restuarant.auth.StaffService getStaff() { return staff; }
    public CancellationService getCancellations() { if(cancellations==null) cancellations=new CancellationService(); return cancellations; }
    public java.util.List<String> getHistory() { if(history==null) history=new java.util.ArrayList<>(); return java.util.Collections.unmodifiableList(history); }
    public void audit(String text) { if(history==null) history=new java.util.ArrayList<>(); history.add(java.time.LocalDateTime.now()+" | "+text); }
    public void integrate(restuarant.auth.StaffService accounts) {
        if(staff==null) staff=accounts;
        if(staff==null) throw new IllegalStateException("Staff registry missing");
        getCancellations(); getBusinessDay(); getHistory(); orderService.bind(this); orderService.reconnect(); getCancellations().bind(orderService); billingService.bind(orderService); billingService.reconnect();
        for(int i=0;i<orderService.getOrderCount();i++) {
            Order o=orderService.getOrderAt(i);
            for(int j=0;j<o.getItemCount();j++) o.getItem(j).recoverInterrupted();
        }
    }
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
