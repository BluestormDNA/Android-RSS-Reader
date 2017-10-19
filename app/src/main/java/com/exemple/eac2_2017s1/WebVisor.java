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
            webView.loadData(generarHtml(item), "text/html; charset=UTF-8", null);
        }
    }

    private String generarHtml(XmlParser.Entrada item) {
        String html = "<h2>" + item.titulo + "</h2>"
                + "<hr>"
                + "<p>" + item.descripcion + "</p>"
                + "<hr>"
                + "<p style='text-align: right'><i>" + item.autor + "</i></p>"
                + "<hr>"
                + "<p>Categorias: " + item.categoria + "</p>"
                + "<p>" + item.fecha + "</p>";
        return html;
    }
}
