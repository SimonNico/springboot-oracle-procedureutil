package com.nico.common.utils;

import java.io.Serializable;
import java.util.List;

public class ListResult<T>implements Serializable {

    private static final long serialVersionUID = 5806884609996390963L;

    private int pages;

    private int pageIndex;

    private int records;

    private List<T> rows;

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getRecords() {
        return records;
    }

    public void setRecords(int records) {
        this.records = records;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }
}
