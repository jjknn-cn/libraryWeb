package com.example.library.entity;

import java.time.LocalDateTime;

/**
 * 借阅记录。
 */
public class Loan extends Base {

    /** 图书副本（不参与 JSON 序列化与通用 JDBC 映射，避免循环引用） */
    private transient BookCopy bookCopy;

    /** 用户（不参与 JSON 序列化与通用 JDBC 映射，避免循环引用） */
    private transient User user;

    /** 借书日期 */
    private LocalDateTime borrowDate;

    /** 还书日期 */
    private LocalDateTime returnDate;

    /** 应还日期（根据图书的 days 属性计算） */
    private LocalDateTime dueDate;

    /** 状态：未还、已还、超期 */
    private String status;

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public void setBookCopy(BookCopy bookCopy) {
        this.bookCopy = bookCopy;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
