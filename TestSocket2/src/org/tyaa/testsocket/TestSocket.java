package org.tyaa.testsocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Демо-проект клиент-сервер (десктоп - десктоп) по WiFi (серверная сторона)
 * @author Юрий
 */
public class TestSocket {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    
    private static boolean mServerActive;
    private static Integer mCounter;
    private static boolean mStopAll;
    private final static ExecutorService mFixedThreadPool;
    private static List<PrintWriter> mWriters;
    
    static {
        mServerActive = true;
        mCounter = 0;
        mStopAll = false;
        mFixedThreadPool = Executors.newFixedThreadPool(10);
        mWriters = new ArrayList<>();
    }
    
    //Точка входа в приложение
    public static void main(String[] args) throws IOException
            , InterruptedException {
        //Создаем объект сервер-сокет для прослушивания порта 3000
        ServerSocket serverSocket = new ServerSocket(3000);
        //Создаем пул потоков выполнения на 10 потоков
        //ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);
        //Создаем объект-семафор с разрешениями для 10 потоков выполнения
        Semaphore semaphore = new Semaphore(10);
        //Запускаем бесконечный цикл прослушивания порта
        while(mServerActive){
            //Выдаем разрешение одному потоку выполнения
            semaphore.acquire();
            //Ожидаем запроса к серверу
            Socket acceptSocket = serverSocket.accept();
            //Когда запрос получен, запускаем в отдельном потоке метод
            //для его обработки, передавая методу объект Socket
            mFixedThreadPool.execute(()->{
                try(Socket _acceptSocket = acceptSocket){
                    mCounter++;
                    serve(_acceptSocket);
                }
                catch(Exception ex){
                    System.out.println(ex);
                }
                //Независимо от того, удалось ли обработать запрос,
                //возвращаем разрешение семафору
                finally{
                    semaphore.release();
                }
            });
            if (mStopAll && mCounter == 0) {
                mServerActive = false;
            }
        }
        System.out.println("Domain stopped");
    }
    //Метод обработки запроса к серверу, который нужно вызывать
    //в отдельном потоке выполнения
    private static void serve(final Socket _acceptSocket) throws IOException{
        //Переменная для получения строки-результата
        String resultLineString;
        //Создаем потоки ввода и вывода, подключенные к объекту Socket
        InputStream inputStream = _acceptSocket.getInputStream();
        OutputStream outputStream = _acceptSocket.getOutputStream();
        //Для упрощения ввода строковых данных от приложения-клиента
        //создаем высокоуровневую обертку потока ввода
        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream));
        //Для упрощения вывода ответа приложению-клиенту
        //создаем высокоуровневую обертку потока вывода
        OutputStreamWriter outputStreamWriter =
                new OutputStreamWriter(outputStream, "utf8");
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        PrintWriter printWriter = new PrintWriter(bufferedWriter, true);
        mWriters.add(printWriter);
        //Читаем потоком ввода текстовые строки, ожидаемые от клиента
        while(true){
            if (mStopAll) {
                mCounter--;
                printWriter.println("The end!");
            } else {
                System.out.println(Thread.currentThread().getName());
                resultLineString = null;
                if (mServerActive) {
                    resultLineString = bufferedReader.readLine();
                } else {
                    printWriter.println("The end!");
                }
                
                
                //Если на текущей итерации от клиента поступила строка
                if(resultLineString != null)
                {
                    if (resultLineString.equals("end")) {
                        printWriter.println("The end!");
                        break;
                    } else if (resultLineString.equals("stop-domain")) {
                        printWriter.println("The end (domain)!");
                        mServerActive = false;
                        
                        for (PrintWriter writer : mWriters) {
                            System.out.println("Kill " + writer);
                            writer.println("The end!");
                        }
                        mFixedThreadPool.shutdownNow();
                        System.out.println("Domain stopped");
                        System.exit(0);
                        
                        break;
                    } else if (resultLineString.equals("stop-all")){
                        printWriter.println("The end!");
                        mCounter--;
                        mStopAll = true;
                        break;
                    } else {
                        //Формируем и отправляем строку-ответ клиенту
                        printWriter.println(resultLineString);
                        //Печатаем принятую от клиента строку в консоль
                        System.out.println(resultLineString);
                    }
                }
            }
        }
    }
}
