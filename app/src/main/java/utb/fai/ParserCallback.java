package utb.fai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Třída ParserCallback je používána parserem DocumentParser,
 * je implementován přímo v JDK a umí parsovat HTML do verze 3.0.
 * Při parsování (analýze) HTML stránky volá tento parser
 * jednotlivé metody třídy ParserCallback, což nám umožuje
 * provádět s částmi HTML stránky naše vlastní akce.
 * 
 * @author Tomá Dulík
 */
class ParserCallback {

    /**
     * pageURI bude obsahovat URI aktuální parsované stránky. Budeme
     * jej využívat pro resolving všech URL, které v kódu stránky najdeme
     * - předtím, než nalezené URL uložžíme do foundURLs, musíme z něj udělat
     * absolutní URL!
     */
    URI pageURI;

    /**
     * depth bude obsahovat aktuální hloubku zanoření
     */
    int depth = 0;
    int maxDepth = 0;

    /**
     * visitedURLs je množina všech URL, které jsme již navštívili
     * (parsovali). Pokud najdeme na stránce URL, který je v této množině,
     * nebudeme jej už dále parsovat
     */
    ConcurrentHashMap<Integer, URI> visitedURIs;

    ConcurrentHashMap<String, Integer> foundWords = new ConcurrentHashMap<>();

    /**
     * foundURLs jsou všechna nová (zatím nenavštívená) URL, která na stránce
     * najdeme. Poté, co projdeme celou stránku, budeme z tohoto seznamu
     * jednotlivá URL brát a zpracovávat.
     */
    LinkedList<URIinfo> foundURIs;

    //HandleLinks handleLinks = new HandleLinks();
    //HandleText handleText = new HandleText();

    ParserCallback(ConcurrentHashMap<Integer, URI> visitedURIs, LinkedList<URIinfo> foundURIs) {
        this.foundURIs = foundURIs;
        this.visitedURIs = visitedURIs;
    }

    /*class HandleText implements Runnable{
        public void run(){*/
        public void handleLinks(){
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(pageURI)
                    .build();

            try{
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.body() == null) {
                    return;
                }

                if(response.statusCode() == 200){
                    Document doc = Jsoup.parse(response.body());

                    String text = doc.body().text();
                    Pattern splitSeparators = Pattern.compile("[^\\p{L}\\p{N}]+");

                    for(String word: splitSeparators.split(text)){
                        if(word != null && !word.isEmpty()){
                            String foundWord = word.toLowerCase();

                            foundWords.merge(foundWord, 1, Integer::sum);
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    //}

    /*class HandleLinks implements Runnable{
        public void run() {*/
        public void handleText(){
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(pageURI)
                    .build();

            try{
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.body() == null) {
                    return;
                }

                if(response.statusCode() == 200){
                    Document doc = Jsoup.parse(response.body());
                    Elements links = doc.select("a");
                    for(Element link : links){
                        if(link.hasAttr("href") && link.attr("href").startsWith("http")){
                            //System.out.println(link.text() + " > " + link.attr("href"));
                            String href = link.attr("href").trim();
                            URI uri = pageURI.resolve(href);
                            foundURIs.add(new URIinfo(uri, depth+1));
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    //}
}
