/*
 * Copyright 2017 plusend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.plusend.sample.R;
import com.plusend.sample.aidl.server.Book;
import com.plusend.sample.aidl.server.BookManagerService;
import com.plusend.sample.aidl.server.IBookManager;
import com.plusend.sample.aidl.server.IOnBookAddedListener;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

public class AidlActivity extends AppCompatActivity {
    private static final String TAG = "AidlActivity";
    private static final int MSG_WHAT_GET_BOOKS = 1;
    private static final int MSG_WHAT_ADD_BOOK = 2;
    private static final int MSG_WHAT_ON_BOOK_ADD = 3;

    private Button addButton, getBooksButton;
    private TextView resultTextView;

    private IBookManager mBookManager;
    private MyHandler mHandler = new MyHandler(this);
    private AddBookRunnable mAddBookRunnable = new AddBookRunnable();
    private GetBooksRunnable mGetBooksRunnable = new GetBooksRunnable();

    private IOnBookAddedListener mIOnBookAddedListener = new IOnBookAddedListener.Stub() {
        @Override
        public void onNewBookAdded(Book book) throws RemoteException {
            Log.d(TAG, "onNewBookAdded: " + book);
            Message message = Message.obtain(mHandler);
            message.what = MSG_WHAT_ON_BOOK_ADD;
            message.obj = book;
            mHandler.sendMessage(message);
        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.d(TAG, "binderDied");
            if (mBookManager == null) {
                return;
            }
            mBookManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mBookManager = null;
            bindService(new Intent(AidlActivity.this, BookManagerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mBookManager = IBookManager.Stub.asInterface(service);
            try {
                service.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                mBookManager.registerBookAddListener(mIOnBookAddedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
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
                    activity.get().resultTextView.append(book + "\n");
                    break;
                case MSG_WHAT_GET_BOOKS:
                    List<Book> bookList = (List<Book>) msg.obj;
                    activity.get().resultTextView.setText("");
                    for (Book item : bookList) {
                        activity.get().resultTextView.append(item.toString() + "\n");
                    }
                    break;
                case MSG_WHAT_ON_BOOK_ADD:
                    Book newBook = (Book) msg.obj;
                    activity.get().resultTextView.append("newBook: " + newBook + "\n");
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
                TaskManager.INSTANCE.post(mAddBookRunnable);
            }
        });

        getBooksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskManager.INSTANCE.post(mGetBooksRunnable);
            }
        });

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

    @Override
    protected void onDestroy() {
        // 防止内存泄漏
        if (mAddBookRunnable != null) {
            TaskManager.INSTANCE.remove(mAddBookRunnable);
        }
        if (mGetBooksRunnable != null) {
            TaskManager.INSTANCE.remove(mGetBooksRunnable);
        }
        if (mIOnBookAddedListener != null && mBookManager.asBinder().isBinderAlive()) {
            try {
                mBookManager.unRegisterBookAddListener(mIOnBookAddedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (mBookManager != null && mBookManager.asBinder().isBinderAlive()) {
            mBookManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
        }
        unbindService(mServiceConnection);
        super.onDestroy();
    }
}
