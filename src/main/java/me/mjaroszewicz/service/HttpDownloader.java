//package me.mjaroszewicz.service;
//
//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpUriRequest;
//import org.apache.http.client.methods.RequestBuilder;
//import org.apache.http.impl.client.HttpClientBuilder;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//
//@Deprecated
//@Service
//public class HttpDownloader {
//
//    private HttpClient httpClient;
//
//    /**
//     * Basic constructor. It instantiates HttpClient
//     */
//    public HttpDownloader(){
//        httpClient = HttpClientBuilder.create().build();
//    }
//
//    /**
//     * Performs http GET request to specified url
//     * @param url valid url
//     * @return String representation of response body
//     * @throws IOException if there is any problem with connection
//     * @throws IllegalStateException when response status is other than 200
//     */
//    public String getStringFromUrl(String url) throws IOException{
//
//        StringBuilder bodyBuilder = new StringBuilder();
//
//        HttpUriRequest request = RequestBuilder.get(url).build();
//        HttpResponse response = httpClient.execute(request);
//
//        if(response.getStatusLine().getStatusCode() != 200)
//            throw new IllegalStateException("Http response status: " + response.getStatusLine());
//
//        //reading response
//        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//        String line;
//        while((line = br.readLine()) != null) {
//            bodyBuilder.append(line).append('\n');
//        }
//
//        return bodyBuilder.toString();
//    }
//
//
//}
