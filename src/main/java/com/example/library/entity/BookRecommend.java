package com.example.library.entity;

/**
 * 教师推荐图书（图书审核记录）。
 */
public class BookRecommend extends Base {

    /** 推荐人 id */
    private String userId;

    /** 书名 */
    private String bookName;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 价格 */
    private Double price;

    /** 状态：待审核、已通过、已驳回 */
    private String status;

    /** 驳回理由 */
    private String reason;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
