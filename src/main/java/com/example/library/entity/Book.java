package com.example.library.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 图书。
 */
public class Book extends Base {

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 类型，包括计算机、文学、历史、经济、哲学、艺术、教育、古典文学、编程语言 */
    private String type;

    /** 出版社 */
    private String publisher;

    /** 还书期限天数 */
    private Integer days;

    /** 价格 */
    private Double price;

    /** 库存（当前可借出的副本数量） */
    private Integer stock;

    /** 图书状态：正常、已废弃 */
    private String status;

    /** 图书简介 */
    private String introduction;

    /** 图书副本集合（不参与 JSON 序列化与通用 JDBC 映射，避免循环引用） */
    private transient List<BookCopy> bookCopies = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public List<BookCopy> getBookCopies() {
        return bookCopies;
    }

    public void setBookCopies(List<BookCopy> bookCopies) {
        this.bookCopies = bookCopies;
    }
}
