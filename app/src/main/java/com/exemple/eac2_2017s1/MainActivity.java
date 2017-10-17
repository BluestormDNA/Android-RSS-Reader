package com.exemple.eac2_2017s1;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.print.PrintDocumentAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import static com.exemple.eac2_2017s1.XmlParser.*;

public class MainActivity extends AppCompatActivity {

    // RSS FEED
    private static final String URL = "http://estaticos.marca.com/rss/portada.xml";
    // Si existeix una connecció wifi
    private static boolean connectatWifi = false;
    // Si existeix una conneció 3G
    private static boolean connectat3G = false;
    //Llista de entrades de noticies
    private List<Entrada> entrades;
    //Gestion layout
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Adaptador adapter;
    private MainActivity main = this;
    //Gestion Base de datos
    DBInterface db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Referenciem el RecyclerView
        recyclerView = (RecyclerView) findViewById(R.id.rView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //afegim l'adaptador amb les dades i el LinearLayoutManager que pintarà les dades
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adaptador(main);
        recyclerView.setAdapter(adapter);

        //Podem utilitzar animacions sobre la llista
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        //DB
        db = new DBInterface(this);
        // PARSER
        //Mirem si hi ha connexió de xarxa
        actualitzaEstatXarxa();
        //Carreguem les noticies a un fil independent fent servir AsyncTask
        cargaNoticias();
        //Després de cada modificació a la font de les dades, hem de notificar-ho a l'adaptador
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void actualitzaEstatXarxa() {
        //Obtenim un gestor de les connexions de xarxa
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //Obtenim l'estat de la xarxa
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                connectatWifi = activeNetwork.isConnected();
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                connectat3G = activeNetwork.isConnected();
            }
        } else {
            // not connected to the internet
        }
    }

    //Fa servir AsyncTask per descarregar el feed XML de stackoverflow.com
    public void cargaNoticias() {

        //Si tenim connexió al dispositiu
        if ((connectatWifi || connectat3G)) {
            new DownloadTask().execute(URL);
        } else {
            Toast.makeText(this, "No hi ha connexio", Toast.LENGTH_LONG).show();
            new CarregaDBTask().execute();
        }
    }

    //Implementació d'AsyncTask per descarregar el feed XML de stackoverflow.com
    private class DownloadTask extends AsyncTask<String, Void, Void> {
        @Override

        //El que s'executar en el background
        protected Void doInBackground(String... urls) {
            try {
                //Carreguem l'XML
                carregaXMLdelaXarxa(urls[0]);
            } catch (IOException | XmlPullParserException e) {
                //Error
            }
            return null;
        }


        @Override
        //Una vegada descarregada la informació XML i convertida a HTML l'enllacem al WebView
        protected void onPostExecute(Void aVoid) {
            if (entrades != null) {
                dbInsertAll(entrades);
                downloadImages(entrades);
            } else {
                Toast.makeText(main, "El resultado esta vacío", Toast.LENGTH_LONG).show();
            }


            //Mostra la cadena HTML en la UI a travs del WebView
            //WebView myWebView = (WebView) findViewById(R.id.webView1);
            //myWebView.loadData(result, "text/html", null);
        }
    }

    private void downloadImages(List result) {
        new DownloadImagesTask().execute(result);
    }

    //Descarrega XML d'stackoverflow.com, l'analitza i crea amb ell un codi HTML que retorna com String
    private Void carregaXMLdelaXarxa(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        //Creem una instncia de l'analitzador
        XmlParser analitzador = new XmlParser();
        //Cadena on construirem el codi HTML que mostrara el widget webView
        StringBuilder htmlString = new StringBuilder();
        try {
            //Obrim la connexio
            //stream = obreConnexioHTTP(urlString);
            stream = ObreConnexioHTTP(urlString);
            //Obtenim la llista d'entrades a partir de l'stream de dades
            entrades = analitzador.analitza(stream);
            //adapter.update(entrades);
            //adapter.notifyDataSetChanged();
        } catch (Exception e) {
            //Toast.makeText(getBaseContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            //Tanquem l'stream una vegada hem terminat de treballar amb ell
            if (stream != null) {
                stream.close();
            }
        }
        return null;
    }

    //analitzador.parse() retorna una llista (entrades) d'entrades de noticies (objectes
    //de la classe Entrada. Cada objecte representa un post de l'XML Feed. Ara es processen
    //les entrades de la llista per crear un codi HTML. Per cada entrada es crea un enllaç
    //a la noticia completa

    //Si tenim noticies
        /*
        if (entrades != null) {

            ///////////////////////////////////////////////////////////
            //Creem l'HTML a partir dels continguts del List<Entrada>//
            ///////////////////////////////////////////////////////////

            //Per indicar quan s'ha actualitzat el RSS
            //Calendar ara = Calendar.getInstance();
            //DateFormat formatData = new SimpleDateFormat("dd MMM h:mmaa");


            //Títol de la pgina
            //htmlString.append("<h3> Noticies </h3>");
            //htmlString.append("<h3>" + getResources().getString(R.string.page_title) + "</h3>");

            //Data d'actualització
            //htmlString.append("<em>Actualitzat el " + formatData.format(ara.getTime()) + "</em>");
            //htmlString.append("<em>" + getResources().getString(R.string.updated) + " " + formatData.format(ara.getTime()) + "</em>");


            //Per cada noticia de la llista
            for (XmlParser.Entrada noticia : entrades) {
                //imagen
                htmlString.append("<img src='" +noticia.imagen + "'>");

                //Creem un títol de la noticia que ser un enllaç HTML a la noticia completa
                htmlString.append("<p> <a href='");
                htmlString.append(noticia.enlace);
                htmlString.append("'>" + noticia.titulo + "</a>");

                //Si la noticia t un resum, l'afegim
                //htmlString.append(noticia.resum);

                //String prova = noticia.resum;
                //String trencat = noticia.descripcion;

                //htmlString.append("<br><i>Resum:</i>" + trencat + "...");
                htmlString.append("</p> <hr>");
            }
        } else {
            htmlString.append("<p> ESTO ESTA VACIO </p>");
        }

        //Retornem un String amb el contingut HTML que mostrar el widget
        //return htmlString.toString();
        return null;
    }
    */

    //Obre una connexió HTTP a partir d'un URL i retorna un InputStream
    private InputStream ObreConnexioHTTP(String adrecaURL) throws IOException {
        InputStream in = null;        //Buffer de recepció
        int resposta = -1;            //Resposta de la connexió

        //Obtenim un URL a partir de l'String proporcionat
        URL url = new URL(adrecaURL);

        //Obtenim una nova connexió al recurs referenciat per la URL
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        try {
            ///////////////////////
            //Preparem la petició//
            ///////////////////////

            httpConn.setReadTimeout(10000);            //Timeout de lectura en milisegons
            httpConn.setConnectTimeout(15000);        //Timeout de connexió en milisegons
            httpConn.setRequestMethod("GET");        //Petició al servidor
            httpConn.setDoInput(true);                //Si la connexió permet entrada

            //Es connecta al recurs.
            httpConn.connect();

            //Obtenim el codi de resposta obtingut del servidor remot HTTP
            resposta = httpConn.getResponseCode();

            //Comprovem si el servidor ens ha retornat un codi de resposta OK,
            //que correspon a que el contingut s'ha descarregat correctament
            if (resposta == HttpURLConnection.HTTP_OK) {
                //Obtenim un Input stream per llegir del servidor
                //in = new BufferedInputStream(httpConn.getInputStream());
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            //Hi ha hagut un problema al connectar
            throw new IOException("Error connectant");
        }

        //Retornem el flux de dades
        return in;
    }

    public void dbInsertAll(List result) {
        ArrayList<Entrada> res = (ArrayList<Entrada>) result;
        db.dropAndRecreateTable();
        db.open();
        for (Entrada r : res) {
            String titulo = r.titulo;
            String enlace = r.enlace;
            String autor = r.autor;
            String descripcion = r.descripcion;
            String fecha = r.fecha;
            String categoria = r.categoria;
            String imagen = r.imagen;
            db.insert(titulo, enlace, autor, descripcion, fecha, categoria, imagen);
        }
        db.close();
    }

    public void onStart() {
        super.onStart();
        //Tornem a actualitzar l'estat de la xarxa
        actualitzaEstatXarxa();
    }

    private class CarregaDBTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            db.open();
            Cursor cursor = db.getAll();
            entrades = new ArrayList<>();
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                String titulo = cursor.getString(cursor.getColumnIndex("titulo"));
                String enlace = cursor.getString(cursor.getColumnIndex("enlace"));
                String autor = cursor.getString(cursor.getColumnIndex("autor"));
                String descripcion = cursor.getString(cursor.getColumnIndex("descripcion"));
                String fecha = cursor.getString(cursor.getColumnIndex("fecha"));
                String categoria = cursor.getString(cursor.getColumnIndex("categoria"));
                String imagen = cursor.getString(cursor.getColumnIndex("imagen"));
                entrades.add(new Entrada(titulo, enlace, autor, descripcion, fecha, categoria, imagen));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            adapter.setList(entrades);
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private class DownloadImagesTask extends AsyncTask<List, Void, Void> {
        // Tasca que fa les operacions de xarxa, no es pot manipular l'UI des d'aquí
        @Override
        protected Void doInBackground(List... url) {
            ArrayList<Entrada> res = (ArrayList<Entrada>) url[0];
            for (Entrada miEntrada : res) {
                Entrada r = (Entrada) miEntrada;
                try {
                    // Agafem la URL que s'ha passat com argument
                    URL imatge = new URL(r.imagen);
                    // Fem la connexió a la URL i mirem la mida de la imatge
                    HttpURLConnection connection = (HttpURLConnection) imatge.openConnection();
                    int totalImatge = connection.getContentLength();
                    // Creem l'input i un buffer on anirem llegint la informació
                    InputStream inputstream = (InputStream) imatge.getContent();
                    byte[] bufferImatge = new byte[1024];
                    // Creem la sortida, és a dir, allà on guardarem la informació (ruta de la imatge)
                    String path = getCacheDir().toString() + File.separator + r.imagen.substring(r.imagen.lastIndexOf('/') + 1, r.imagen.length());
                    OutputStream outputstream = new FileOutputStream(path);
                    int descarregat = 0;
                    int count;
                    // Mentre hi hagi informació que llegir
                    while ((count = inputstream.read(bufferImatge)) != -1) {
                        // Acumulem tot el que ha llegit
                        descarregat += count;
                        // Guardem al disc el que hem descarregat
                        outputstream.write(bufferImatge, 0, count);
                    }
                    // Tanquem els "stream"
                    inputstream.close();
                    outputstream.close();
                } catch (IOException exception) {
                    Log.d("ERR", "Alguna cosa no ha anat bé!");
                    return null;
                }
            }
            // No passem cap informació al onPostExecute
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            adapter.setList(entrades);
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}


