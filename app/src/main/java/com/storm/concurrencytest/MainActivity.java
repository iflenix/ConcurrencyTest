package com.storm.concurrencytest;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
    final int FILE_QUEUE_SIZE = 10;
    final int SEARCH_THREADS = 100;
    BlockingQueue<File> fileQueue = new ArrayBlockingQueue<>(FILE_QUEUE_SIZE);

    public void onBlQueueButtonClick(View view) {



        FileEnumerationTask enumerator = new FileEnumerationTask(fileQueue,Environment.getExternalStorageDirectory());


        new Thread(enumerator).start();

        for (int i = 1; i <= SEARCH_THREADS; i++) {
            new Thread(new SearchTask(fileQueue,"100500")).start();
        }
    }

    public void onStopSearchButtonClick(View view) {
        try {
            fileQueue.put(FileEnumerationTask.DUMMY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class FileEnumerationTask implements Runnable {

    public static File DUMMY = new File("");
    private BlockingQueue<File> queue;
    private File startingDirectory;

    /**
     * @param queue             Блокирующая очередь, в которую вводятся перечисляемые файлы
     * @param startingDirectory Каталог, с которого начинается перечисление файлов
     */
    public FileEnumerationTask(BlockingQueue<File> queue, File startingDirectory) {
        this.queue = queue;
        this.startingDirectory = startingDirectory;

    }

    @Override
    public void run() {
        try {
            enumerate(startingDirectory);
            queue.put(DUMMY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public void enumerate(File directory) throws InterruptedException {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (!file.isDirectory())
                queue.put(file);
        }
    }
}

class SearchTask implements Runnable {

    private BlockingQueue<File> queue;
    private String keyword;

    public SearchTask(BlockingQueue<File> queue, String keyword) {
        this.queue = queue;
        this.keyword = keyword;
    }

    public void search(File file) throws IOException {
        try (Scanner in = new Scanner(file)) {
            int lineNumber = 0;
            while (in.hasNextLine()) {
                lineNumber++;
                String line = in.nextLine();
                if (line.contains(keyword)) {
                    Log.d("MY_TAG", String.format("%s:%d:%s%n", file.getPath(), lineNumber, line));
                }

            }
        }

    }

    @Override
    public void run() {

        try {
            boolean done = false;
            while (!done) {
                File file = queue.take();
                if (file == FileEnumerationTask.DUMMY) {
                    done = true;
                    queue.put(file);
                } else {
                    search(file);
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }


}
