
package com.nexora.launcher;

public class UpdaterTest {

    public static void main(String[] args) {
        System.out.println("Nexora Update Check");
        System.out.println("-------------------");

        String result = Updater.checkForUpdates();

        System.out.println(result);
    }
}
