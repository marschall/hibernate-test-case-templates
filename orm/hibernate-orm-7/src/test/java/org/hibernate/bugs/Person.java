package org.hibernate.bugs;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
public class Person implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private int version;

    //@Fetch(FetchMode.SELECT)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = true, mappedBy = "person")
    private Author author;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
