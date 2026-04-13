package com.example.service;

import com.example.dto.Movie;
import com.example.exception.MovieErrorResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;

import static com.example.constants.MoviesAppConstants.ADD_MOVIE_V1;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class MoviesRestClientJunit5Test {

    static WireMockServer wireMockServer;

    @Autowired
    MoviesRestClient moviesRestClient;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("movieapp.baseUrl", () -> "http://localhost:8090");
    }

    @BeforeEach
    void reset() {
        wireMockServer.resetAll();
    }

    @Test
    void getAllMovies() {
        stubFor(get(anyUrl()).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(200)
                .withBodyFile("all-movies.json")));

        List<Movie> movies = moviesRestClient.retrieveAllMovies();
        assertFalse(movies.isEmpty());
    }

    @Test
    void retrieveMovieById() {
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));

        Movie movie = moviesRestClient.retrieveMovieById(1);
        assertEquals("Batman Begins", movie.getName());
    }

    @Test
    void retrieveMovieById_NotFound() {
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+")).willReturn(aResponse()
                .withStatus(404)
                .withBodyFile("404-movieId.json")));

        assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieById(100));
    }

    @Test
    void addNewMovie() {
        Movie movieRequest = new Movie(
                null,
                "Toy Story 4",
                2019,
                "Tom Hanks, Tim Allen",
                LocalDate.of(2019, 6, 20)
        );

        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name"))
                .withRequestBody(matchingJsonPath("$.cast"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        Movie response = moviesRestClient.addNewMovie(movieRequest);
        assertNotNull(response.getMovie_id());
    }

    @Test
    void deleteMovie() {
        stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Movie Deleted Successfully")));

        String response = moviesRestClient.deleteMovieById(1);
        assertEquals("Movie Deleted Successfully", response);
    }
}
