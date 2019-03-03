/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tyaa.testsocketclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author student
 */
public class BackgroundReader extends Thread{

    private BufferedReader mReader;
    private String mResponseString;
    private Globals mGlobals;

    public BackgroundReader(BufferedReader reader, String responseString, Globals globals) {
        this.mReader = reader;
        this.mResponseString = responseString;
        this.mGlobals = globals;
        this.setDaemon(true);
        this.start();
    }
    
    @Override
    public void run() {
        while(mGlobals.active){
            try {
                //Читаем потоком ввода строку-ответ сервера
                mResponseString = mReader.readLine();
            } catch (IOException ex) {
                Logger.getLogger(BackgroundReader.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Server: " + mResponseString);
            System.out.print("> ");
            if (mResponseString.equals("The end!") || mGlobals.equals("stop-domain")) {
                mGlobals.active = false;
                System.exit(0);
            }
        }
    }
    
}
