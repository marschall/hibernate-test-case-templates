package org.hibernate.bugs;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
public class Author implements Serializable {

    @Id
    @GeneratedValue
    Long id;

    @Version
    private int version;

    @OneToOne(optional = false)
    @JoinColumn(name = "person_id", unique = true, nullable = false, referencedColumnName = "id")
    private Person person;

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
