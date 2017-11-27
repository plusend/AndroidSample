package com.plusend.sample.aidl.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.plusend.sample.R;
import com.plusend.sample.aidl.server.Book;
import com.plusend.sample.aidl.server.BookManagerService;
import com.plusend.sample.aidl.server.IBookManager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

public class AidlActivity extends AppCompatActivity {
    private static final int MSG_WHAT_GET_BOOKS = 1;
    private static final int MSG_WHAT_ADD_BOOK = 2;

    private Button addButton, getBooksButton;
    private TextView resultTextView;

    private IBookManager mBookManager;
    private MyHandler mHandler;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBookManager = IBookManager.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBookManager = null;
        }
    };

    public static class MyHandler extends Handler {
        private WeakReference<AidlActivity> activity;

        MyHandler(AidlActivity aidlActivity) {
            activity = new WeakReference<>(aidlActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (activity.get() == null || activity.get().isFinishing()) {
                return;
            }

            switch (msg.what) {
                case MSG_WHAT_ADD_BOOK:
                    Book book = (Book) msg.obj;
                    activity.get().resultTextView.setText(book.toString());
                    break;
                case MSG_WHAT_GET_BOOKS:
                    List<Book> bookList = (List<Book>) msg.obj;
                    activity.get().resultTextView.setText("");
                    for (Book item : bookList) {
                        activity.get().resultTextView.append(item.toString() + "\n");
                    }
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aidl);

        addButton = findViewById(R.id.add);
        getBooksButton = findViewById(R.id.get_books);
        resultTextView = findViewById(R.id.result);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskManager.INSTANCE.post(new AddBookRunnable());
            }
        });

        getBooksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskManager.INSTANCE.post(new GetBooksRunnable());
            }
        });

        mHandler = new MyHandler(this);

        bindService(new Intent(this, BookManagerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private class AddBookRunnable implements Runnable {

        @Override
        public void run() {
            if (mBookManager == null) {
                return;
            }
            try {
                Book book = newBook();
                mBookManager.addBook(book);

                Message message = Message.obtain(mHandler);
                message.what = MSG_WHAT_ADD_BOOK;
                message.obj = book;
                mHandler.sendMessage(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetBooksRunnable implements Runnable {

        @Override
        public void run() {
            if (mBookManager == null) {
                return;
            }
            try {
                List<Book> bookList = mBookManager.getBooks();

                Message message = Message.obtain(mHandler);
                message.what = MSG_WHAT_GET_BOOKS;
                message.obj = bookList;
                mHandler.sendMessage(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private Book newBook() {
        int id = new Random().nextInt(100);
        return new Book(id, "book: " + id);
    }
}
