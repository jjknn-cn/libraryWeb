package com.example.library.entity;

/**
 * 图书副本：一本图书可以有多本副本，借还书以副本为单位。
 */
public class BookCopy extends Base {

    /** 属于哪种书（不参与 JSON 序列化与通用 JDBC 映射，避免循环引用） */
    private transient Book book;

    /** 借阅状态：true 为已借出，false 为未借阅 */
    private boolean isBorrowed;

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public boolean isBorrowed() {
        return isBorrowed;
    }

    public void setBorrowed(boolean borrowed) {
        isBorrowed = borrowed;
    }
}
