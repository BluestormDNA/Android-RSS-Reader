package com.exemple.eac2_2017s1;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BlueStorm on 12/10/2017.
 */

public class XmlParser {

    // No fem servir namespaces
    private static final String ns = null;

    //Aquesta classe representa una entrada de noticia del RSS Feed
    public static class Entrada {
        public final String titulo;
        public final String enlace;
        public final String autor;
        public final String descripcion;
        public final String fecha;
        public final String categoria;
        public final String imagen;

        public Entrada(String titulo, String descripcion, String enlace, String autor, String fecha, String categoria, String imagen) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.enlace = enlace;
            this.autor = autor;
            this.fecha = fecha;
            this.categoria = categoria;
            this.imagen = imagen;
        }
    }

    public List<Entrada> analitza(InputStream in) throws XmlPullParserException, IOException {
        try {
            //Obtenim analitzador
            XmlPullParser parser = Xml.newPullParser();
            //No fem servir namespaces
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            //Especifica l'entrada de l'analitzador
            parser.setInput(in, null);
            //Obtenim la primera etiqueta
            parser.nextTag();
            //Retornem la llista de noticies
            return leerNoticias(parser);
        } finally {
            in.close();
        }
    }


    //Llegeix una llista de noticies d'StackOverflow a partir del parser i retorna una llista d'Entrades
    private List<Entrada> leerNoticias(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Entrada> listaItems = new ArrayList<Entrada>();
        //Comprova si l'event actual és del tipus esperat (START_TAG) i del nom "feed"
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, ns, "channel");
        //Mentre que no arribem al final d'etiqueta

        while (parser.next() != XmlPullParser.END_TAG) {
            //Ignorem tots els events que no siguin un comenament d'etiqueta
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                //Saltem al seguent event
                continue;
            }
            //Obtenim el nom de l'etiqueta
            String name = parser.getName();
            // Si aquesta etiqueta és una entrada de noticia
            if (name.equals("item")) {
                //Afegim l'entrada a la llista
                listaItems.add(leerItem(parser));
            } else {
                saltar(parser);
            }
        }
        return listaItems;
    }


    //Aquesta funció serveix per saltar-se una etiqueta i les seves subetiquetes aniuades.
    private void saltar(XmlPullParser parser) throws XmlPullParserException, IOException {
        //Si no és un comenament d'etiqueta: ERROR
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;

        //Comprova que ha passat per tantes etiquetes de començament com acabament d'etiqueta

        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    //Cada vegada que es tanca una etiqueta resta 1
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    //Cada vegada que s'obre una etiqueta augmenta 1
                    depth++;
                    break;
            }
        }
    }

    //Analitza el contingut d'una entrada. Si troba un ttol, resum o enllaç, crida els mètodes de lectura
    //propis per processar-los. Si no, ignora l'etiqueta.
    private Entrada leerItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        String titulo = null;
        String enlace = null;
        String autor = null;
        String descripcion = null;
        String fecha = null;
        String categoria = null;
        String imagen = null;

        //L'etiqueta actual ha de ser "item"
        parser.require(XmlPullParser.START_TAG, ns, "item");

        //Mentre que no acabe l'etiqueta de "item"
        while (parser.next() != XmlPullParser.END_TAG) {
            //Ignora fins que no trobem un començament d'etiqueta
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            //Obtenim el nom de l'etiqueta
            String etiqueta = parser.getName();
            if (etiqueta.equals("title")) { //titulo noticia
                titulo = leerEtiqueta(parser, "title");
            } else if (etiqueta.equals("description")) { // resumen
                descripcion = leerEtiqueta(parser, "description");
            } else if (etiqueta.equals("link")) { //enlace
                enlace = leerEtiqueta(parser, "link");
            } else if (etiqueta.equals("pubDate")) { //fecha
                fecha = leerEtiqueta(parser, "pubDate");
            } else if (etiqueta.equals("dc:creator")) { //autor
                autor = leerEtiqueta(parser, "dc:creator");
            } else if (etiqueta.equals("category")) { //categoria
                if (categoria == null) {
                    categoria = (leerEtiqueta(parser, "category"));
                } else {
                    categoria += (", " + leerEtiqueta(parser, "category"));
                }
            } else if (etiqueta.equals("media:thumbnail")) { //imagen
                imagen = leerImagen(parser);
            } else {
                //les altres etiquetes les saltem
                saltar(parser);
            }
        }
        //Creem una nova entrada amb aquestes dades i la retornem
        return new Entrada(titulo, descripcion, enlace, autor, fecha, categoria, imagen);
    }

    private String leerEtiqueta(XmlPullParser parser, String etiqueta) throws IOException, XmlPullParserException {
        //L'etiqueta actual ha de ser "pubDate"
        parser.require(XmlPullParser.START_TAG, ns, etiqueta);
        //Llegeix
        String contenido = llegeixText(parser);
        //Fi d'etiqueta
        parser.require(XmlPullParser.END_TAG, ns, etiqueta);
        return contenido;
    }

    private String leerImagen(XmlPullParser parser) throws IOException, XmlPullParserException {
        //L'etiqueta actual ha de ser "media:thumbnail"
        parser.require(XmlPullParser.START_TAG, ns, "media:thumbnail");
        //Llegeix atribut URL
        String imagen = parser.getAttributeValue(null, "url");
        //Fi d'etiqueta no tiene
        parser.next();
        return imagen;
    }

    //Extrau el valor de text per les etiquetes titol, resum
    private String llegeixText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String resultat = "";

        if (parser.next() == XmlPullParser.TEXT) {
            resultat = parser.getText();
            parser.nextTag();
        }
        return resultat;
    }

}