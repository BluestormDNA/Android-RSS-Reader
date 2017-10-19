package com.exemple.eac2_2017s1;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by BlueStorm on 19/10/2017.
 */

public class WebVisor extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        XmlParser.Entrada item = (XmlParser.Entrada) getIntent().getSerializableExtra("item");
        WebView webView = (WebView) findViewById(R.id.webVisor);

        getSupportActionBar().setTitle(item.titulo);

        if (Util.hayConexion(this)) {
            //generamos el webview y cargamos el enlace
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(item.enlace);
        } else {
            //carga html generado al webview
        }
    }
}
