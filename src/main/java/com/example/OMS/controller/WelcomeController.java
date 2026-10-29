package com.example.OMS.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Order Management API</title>
                </head>
                <body>
                    <h1>Order Management API</h1>
                    <p>Welcome! The service is up and running.</p>
                    <ul>
                        <li><a href="/api/customers">Customers</a></li>
                        <li><a href="/api/products">Products</a></li>
                        <li><a href="/api/orders">Orders</a></li>
                    </ul>
                </body>
                </html>
                """;
    }
}
