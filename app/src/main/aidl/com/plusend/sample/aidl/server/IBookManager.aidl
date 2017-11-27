// IBookManager.aidl
package com.plusend.sample.aidl.server;

// Declare any non-default types here with import statements
import com.plusend.sample.aidl.server.Book;

interface IBookManager {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    List<Book> getBooks();

    void addBook(in Book book);
}
