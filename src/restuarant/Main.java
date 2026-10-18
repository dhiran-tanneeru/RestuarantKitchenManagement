package restuarant;

import restuarant.model.*;

public class Main {
    public static void main(String[] args){

        System.out.println("Restuarant Management System");
        System.out.println("----------------------------");

        Menuitem biryani = new Menuitem( 101 , "Chicken Biryani","Main Course", 250, 25);
        Menuitem paneer = new Menuitem(102, "Paneer", "Starters", 180, 15);

        System.out.println("\nMENU:");
        System.out.println(biryani);
        System.out.println(paneer);

        RestuarantTable table = new RestuarantTable(7, 4);

        System.out.println("\nINITIAL TABLE STATUS:");
        System.out.println(table);

        table.seatGuests(4,201);

        System.out.println("\nAFTER SEATING CUSTOMERS:");
        System.out.println(table);

        try {
            table.seatGuests(2, 202);
        } catch (IllegalStateException e) {
            System.out.println(
                "\nExpected error: " + e.getMessage()
            );
        }

        table.markForCleaning();

        System.out.println("\nTABLE UNDER CLEANING:");
        System.out.println(table);

        table.finishCleaning();

        System.out.println("\nAFTER CLEANING:");
        System.out.println(table);
    }
    
}
