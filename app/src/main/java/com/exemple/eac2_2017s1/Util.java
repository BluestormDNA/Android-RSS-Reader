package com.exemple.eac2_2017s1;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by BlueStorm on 19/10/2017.
 */

public class Util {

    public static boolean hayConexion(Activity activity) {
        boolean hayConexion = false;
        //Obtenim un gestor de les connexions de xarxa
        ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        //Obtenim l'estat de la xarxa
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            hayConexion = activeNetwork.isConnected();
        }
        return hayConexion;
    }
}
