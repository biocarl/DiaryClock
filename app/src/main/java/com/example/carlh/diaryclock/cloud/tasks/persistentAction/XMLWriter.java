package com.example.carlh.diaryclock.cloud.tasks.persistentAction;

import android.content.Context;
import android.util.Log;

import com.example.carlh.diaryclock.app.DiaryClock;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by carlh on 23.05.2017.
 */

public class XMLWriter {

    private String file;
    private Context context;

    public  XMLWriter(Context context){
        this.context = context;

        //Getting application
        DiaryClock application = ((DiaryClock) context.getApplicationContext());
        String systemRoot = application.getRootPath().getAbsolutePath();
        this.file = systemRoot+"/"+ActionHelper.FILE;


        //Create XML-file if it doesn't exist
        File f = new File(file);

        //create parent folder
        boolean allFoldersCreated = f.getParentFile().mkdirs(); //TODO find out where it is best to create the basic system folder logic

        if(!f.exists()) {
            try {
                Log.e(getClass().getName(), "Creating XML",null);
                createXML(file);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        }
    }

    /*Writes new child directly to file*/
    public void writeEntries(ArrayList<Action> actionList){
        //annotate to existing xml-file

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = null;
            documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(new File(file));
            Element root = document.getDocumentElement();


            for(Action action : actionList){

                // action type
                Element newAction = document.createElement("action");

                Element type = document.createElement("type");
                type.appendChild(document.createTextNode(action.getType().name()));
                newAction.appendChild(type);

                Element path = document.createElement("id");
                path.appendChild(document.createTextNode(String.valueOf(action.getId())));
                newAction.appendChild(path);
                root.appendChild(newAction);
            }

             //Save changes to file
            DOMSource source = new DOMSource(document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }


    }

    /*Create default XML-Header*/
    private void createXML(String file) throws ParserConfigurationException, TransformerException {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        // Root Element
        Element rootElement = document.createElement("Actions");
        document.appendChild(rootElement);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);

        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }





}