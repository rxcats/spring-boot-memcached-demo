package io.github.rxcats.springbootmemcacheddemo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import io.sixhours.memcached.cache.MemcachedCacheManager;

@Slf4j
@EnableCaching
@SpringBootApplication
public class SpringBootMemcachedDemoApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpringBootMemcachedDemoApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }

    @Data
    static class Book implements Serializable {

        private static final long serialVersionUID = 1795480103996348045L;

        private String isbn;

        private String title;

        private String author;

        public static Book dummy(String isbn, String title, String author) {
            var book = new Book();
            book.isbn = isbn;
            book.title = title;
            book.author = author;
            return book;
        }

    }

    @Service
    static class BookService {

        private final Map<String, Book> dummyBooks = new HashMap<>();

        public BookService() {
            dummyBooks.put("9780345538376",
                Book.dummy("9780345538376", "The Hobbit and the Lord of the Rings (the Hobbit / the Fellowship of the Ring / the Two Towers / the", "Author: J.R.R. Tolkien"));
            dummyBooks.put("9780395489321",
                Book.dummy("9780395489321", "The Lord of the Rings", "Author: J.R.R. Tolkien"));
            dummyBooks.put("9780679723257",
                Book.dummy("9780679723257", "The Postman Always Rings Twice", "James M. Cain"));
        }

        @Cacheable(value = "book"/*, key = "#isbn"*/)
        public Book getBook(String isbn) {
            var book = dummyBooks.get(isbn);
            log.info("book:{}", book);
            return book;
        }
    }

    @Service
    static class CacheValueService {

        @Resource(type = MemcachedCacheManager.class)
        MemcachedCacheManager cacheManager;

        public Cache getCache(String name) {
            return cacheManager.getCache(name);
        }

        public <T> T get(String name, String key, Class<T> type) {
            Cache cache = cacheManager.getCache(name);
            if (cache == null) {
                return null;
            }
            return cache.get(key, type);
        }

        public void set(String name, String key, Object value) {
            Cache cache = cacheManager.getCache(name);
            if (cache == null) {
                return;
            }
            cache.put(key, value);
        }
    }

    @Autowired
    BookService bookService;

    @Autowired
    CacheValueService cacheValueService;

    @Bean
    CommandLineRunner runner() {
        return args -> {

            bookService.getBook("9780679723257");
            bookService.getBook("9780679723257");
            log.info("cachedBook:{}", cacheValueService.get("book", "9780679723257", Book.class));

        };
    }

}

