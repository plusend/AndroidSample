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

package com.plusend.sample.aidl.server;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BookManagerService extends Service {
    public static final String TAG = "BookManagerService";

    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>();
    private RemoteCallbackList<IOnBookAddedListener> mBookAddListenerList = new RemoteCallbackList<>();

    public BookManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (checkCallingOrSelfPermission("com.plusend.sample.permission.ACCESS_BOOK_SERVICE") == PackageManager.PERMISSION_DENIED) {
            return null;
        }
        return mBinder;
    }

    private Binder mBinder = new IBookManager.Stub() {
        @Override
        public List<Book> getBooks() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);

            final int N = mBookAddListenerList.beginBroadcast();
            Log.d(TAG, "mBookAddListenerList size: " + N);
            for (int i = 0; i < N; i++) {
                IOnBookAddedListener listener = mBookAddListenerList.getBroadcastItem(i);
                if (listener != null) {
                    listener.onNewBookAdded(book);
                }
            }
            mBookAddListenerList.finishBroadcast();
        }

        @Override
        public void registerBookAddListener(IOnBookAddedListener listener) throws RemoteException {
            mBookAddListenerList.register(listener);
        }

        @Override
        public void unRegisterBookAddListener(IOnBookAddedListener listener) throws RemoteException {
            mBookAddListenerList.unregister(listener);
        }
    };
}
