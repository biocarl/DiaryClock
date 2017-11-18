package com.example.carlh.diaryclock.cloud.tasks.persistentAction;

import android.content.Context;
import android.util.Log;

import com.example.carlh.diaryclock.app.DiaryClock;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
public class XMLParser {

    /*all the parsed actions*/
    private ArrayList<Action> actions;
    private boolean hasActions;
    private String file;


    /*Parse file*/
    public XMLParser(Context context){
        //Getting application
        DiaryClock application = ((DiaryClock) context.getApplicationContext());
        String systemRoot = application.getRootPath().getAbsolutePath();
        this.file = systemRoot+"/"+ActionHelper.FILE;

        //Log.e(getClass().getName(), "[true]action.xml : "+file);

        //Create XML-file if it doesn't exist
        File f = new File(file);
        if(f.exists() && !f.isDirectory()) {
            //parse file
            actions = parseActions();
            //check for content
            hasActions = !actions.isEmpty();
        }else{
            hasActions = false;
        }
    }

    /*parse Actions from XML-File*/
    private ArrayList<Action> parseActions(){

        //Declaration
        ArrayList<Action> actionList = new ArrayList<>();

        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            //Declarations//Init
            Element element;
            Node node;

            //Get all actions
            NodeList nodeList = doc.getElementsByTagName("action");
            for (int i = 0; i < nodeList.getLength(); i++) {
                node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    element = (Element) node ;
                    actionList.add( new Action(ActionHelper.Type.fromString(element.getElementsByTagName("type").item(0).getTextContent())
                            ,Long.parseLong(element.getElementsByTagName("id").item(0).getTextContent())));
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return actionList;
    }

    /*Get parsed actions*/
    public ArrayList<Action> getActions() {
        //actions available
        if(hasActions) {
            return actions;
        }else {
            return null;
        }
    }

    /*
    hasActions parses if boolean is not set!
     */

    public boolean hasActions() {
        return hasActions;
    }


    /*Delete first element of XML-File: Working of like a Stack, which is persistent*/
    public void removeFirst(){

        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            //Get the first Node with action
            Node node = doc.getElementsByTagName("action").item(0);
            Element table = doc.getDocumentElement();
            table.removeChild(node);

            //Save changes to file
            DOMSource source = new DOMSource(doc);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }



}
