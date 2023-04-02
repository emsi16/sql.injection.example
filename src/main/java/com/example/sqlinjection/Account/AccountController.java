package com.example.sqlinjection.Account;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class AccountController {

    @Autowired AccountRepository accountRepository;
    @Autowired DataSource dataSource;

    //DOCKER POSTGRES:
    // docker run -d -e POSTGRES_PASSWORD=password -e POSTGRES_USER=sql -e POSTGRES_DB=sql -p 12345:5432 postgres:14.5

    @GetMapping("/list")
    public List<Account> listAccount(){

        return accountRepository.findAll();

    }

    //typial usage:
    //https://localhost:30000/unsafe-get/1
    //inection: https://localhost:30000/unsafe-get/1' or '1'='1
    @GetMapping("/unsafe-get/{id}")
    public List<Account> listAccountUnsafe(@PathVariable  String id) throws SQLException {
        log.info("var: {}", id);

        String sql = "select "
                + "* "
                + "from accounts where id = '"
                + id
                + "'";
        Connection c = dataSource.getConnection();
        ResultSet rs = c.createStatement().executeQuery(sql);

        List<Account> accounts = new ArrayList<>();

        while (rs.next()){
            accounts.add(Account.builder()
                            .id(rs.getObject("id", Long.class))
                            .name(rs.getObject("name", String.class))
                            .surname(rs.getObject("surname", String.class))
                    .build());
        }

        rs.close();

        return accounts;

    }


    @GetMapping("/safe-get/{name}")
    public List<Account> listAccountSafe(@PathVariable String name) throws SQLException {
        log.info("var: {}", name);

        //parameterized queries
        String sql = "select "
                + "* "
                + "from accounts where name = ?";

        Connection c = dataSource.getConnection();
        PreparedStatement p = c.prepareStatement(sql);
        p.setString(1, name);
        ResultSet rs = p.executeQuery();

        List<Account> accounts = new ArrayList<>();

        while (rs.next()){
            accounts.add(Account.builder()
                    .id(rs.getObject("id", Long.class))
                    .name(rs.getObject("name", String.class))
                    .surname(rs.getObject("surname", String.class))
                    .build());
        }

        rs.close();

        return accounts;

    }

    //typical usage:
    //https://localhost:30000/unsafe-update?name=krzysztof&surname=nowak&id=1
    //inection: https://localhost:30000/unsafe-update?name=krzysztof&surname=nowak&id=1' or '1' = '1
    @PostMapping("/unsafe-update")
    public String updateUnsafe(String id, String name, String surname) throws SQLException {
        String sql = "UPDATE accounts "
                + "set name = '"+name+"',"
                + "surname = '"+surname+"'"
                + "where id = '"
                + id
                + "'";

        log.info("SQL: {}", sql);
        Connection c = dataSource.getConnection();
        c.createStatement().executeUpdate(sql);

        return "Updated account with id: " + id;

    }

    //typical usage:
    //https://localhost:30000/safe-update?name=krzysztof&surname=nowak&id=1
    //inection: https://localhost:30000/safe-update?name=krzysztof&surname=nowak&id=1' or '1' = '1 -> błąd
    @PostMapping("/safe-update")
    public String updateSafe(String id, String name, String surname) throws SQLException {
        String sql = "UPDATE accounts "
                + "set name = ?,"
                + "surname = ?"
                + "where id = ?";

        //parameterized queries
        Connection c = dataSource.getConnection();
        PreparedStatement p = c.prepareStatement(sql);
        p.setString(1, name);
        p.setString(2, surname);
        p.setLong(3, Long.parseLong(id));
        p.executeUpdate();

        return "Updated account with id: " + id;
    }


    //typical usage: https://localhost:30000/unsafe-delete?id=1
    //injection: https://localhost:30000/unsafe-delete?id=1' or '1'  = '1
    @PostMapping("/unsafe-delete")
    public String deleteUnsafe(String id) throws SQLException {
        String sql = "DELETE from accounts "
                + "where id = '"
                + id
                + "'";

        log.info("SQL: {}", sql);
        Connection c = dataSource.getConnection();
        c.createStatement().execute(sql);

        return "Deleted account with id: " + id;
    }
    //typical usage: https://localhost:30000/unsafe-delete?id=1
    //injection: https://localhost:30000/unsafe-delete?id=1' or '1'  = '1 -> błąd
    @PostMapping("/safe-delete")
    public String deleteSafe(String id, String name, String surname) throws SQLException {
        String sql = "DELETE from accounts "
                + "where id = ?";

        //parameterized queries
        Connection c = dataSource.getConnection();
        PreparedStatement p = c.prepareStatement(sql);
        p.setLong(1, Long.parseLong(id));
        p.execute();

        return "Deleted account with id: " + id;
    }

}
