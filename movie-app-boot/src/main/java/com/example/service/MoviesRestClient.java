package com.example.service;

import com.example.dto.Movie;
import com.example.exception.MovieErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.constants.MoviesAppConstants.ADD_MOVIE_V1;
import static com.example.constants.MoviesAppConstants.GET_ALL_MOVIES_V1;
import static com.example.constants.MoviesAppConstants.MOVIE_BY_ID_PATH_PARAM_V1;
import static com.example.constants.MoviesAppConstants.MOVIE_BY_NAME_PATH_PARAM_V1;
import static com.example.constants.MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1;
import static com.example.constants.MoviesAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1;

@Slf4j
@Component
public class MoviesRestClient {

    @Autowired
    private WebClient webClient;

    public List<Movie> retrieveAllMovies() {
        return webClient.get().uri(GET_ALL_MOVIES_V1)
                .retrieve() // actual call is made to the api
                .bodyToFlux(Movie.class) //body is converted to flux(Represents multiple items)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClientResponseException - {}", ex.getResponseBodyAsString(), ex);
                    return Flux.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
                .collectList() // collecting the httpResponse as a list\
                .block(); // This call makes the Webclient to behave as a synchronous client.
    }

    public Movie retrieveMovieById(Integer movieId) {

        return webClient.get()
                .uri("/movieservice/v1/movie/{id}", movieId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .map(body -> new MovieErrorResponse("Movie not found")))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .map(body -> new MovieErrorResponse("Server error")))
                .bodyToMono(Movie.class)
                .block();
    }

    public List<Movie> retrieveMovieByName(String movieName) {

        String retrieveByNameUri = UriComponentsBuilder.fromUriString(MOVIE_BY_NAME_QUERY_PARAM_V1)
                .queryParam("movie_name", movieName)
                .buildAndExpand()
                .toUriString();

        return webClient.get().uri(retrieveByNameUri)
                .retrieve()
                .bodyToFlux(Movie.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClientResponseException - {}", ex.getResponseBodyAsString(), ex);
                    return Flux.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
                .collectList()
                .block();
    }

    public List<Movie> retrieveMovieByYear(Integer year) {
        String retrieveByYearUri = UriComponentsBuilder.fromUriString(MOVIE_BY_YEAR_QUERY_PARAM_V1)
                .queryParam("year", year)
                .buildAndExpand()
                .toUriString();

        return webClient.get().uri(retrieveByYearUri)
                .retrieve()
                .bodyToFlux(Movie.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClientResponseException - {}", ex.getResponseBodyAsString(), ex);
                    return Flux.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
                .collectList()
                .block();
    }

    public Movie addNewMovie(Movie newMovie) {
        return webClient.post()
                .uri(ADD_MOVIE_V1)
                .bodyValue(newMovie)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(MovieErrorResponse::new)
                                .flatMap(Mono::error)
                )
                .bodyToMono(Movie.class)
                .block();
    }

    public Movie updateMovie(Integer movieId, Movie movie) {
        return webClient.put()
                .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                .bodyValue(movie)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(MovieErrorResponse::new)
                                .flatMap(Mono::error)
                )
                .bodyToMono(Movie.class)
                .block();
    }

    public String deleteMovieById(Integer movieId) {
        return webClient.delete()
                .uri("/movieservice/v1/movie/{id}", movieId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new MovieErrorResponse("Movie API error"))
                )
                .bodyToMono(String.class)
                .block();
    }

    public String deleteMovieByName(String movieName) {
        webClient.delete()
                .uri(MOVIE_BY_NAME_PATH_PARAM_V1, movieName)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")   // ensures a value (empty string) if no body
                                .map(MovieErrorResponse::new)
                                .flatMap(Mono::error)
                )
                .bodyToMono(Void.class)
                .block();

        return "Movie Deleted SuccessFully";
    }
}
