package com.learnwiremock.service;

import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.learnwiremock.constants.MovieAppConstants.ADD_MOVIE_V1;
import static com.learnwiremock.constants.MovieAppConstants.GET_ALL_MOVIES_V1;
import static com.learnwiremock.constants.MovieAppConstants.MOVIE_BY_ID_PATH_PARAM_V1;
import static com.learnwiremock.constants.MovieAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1;
import static com.learnwiremock.constants.MovieAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1;

@Slf4j
public class MoviesRestClient {
    private final WebClient webClient;

    public MoviesRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

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
        return webClient.get().uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId) //mapping the movie id to the url
                .retrieve()
                .bodyToMono(Movie.class) //body is converted to Mono(Represents single item)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClientResponseException - {}", ex.getResponseBodyAsString(), ex);
                    return Mono.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
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
                    return Mono.error(new MovieErrorResponse(ex.getStatusText(), ex));
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
                    return Mono.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
                .collectList()
                .block();

    }

    public Movie addNewMovie(Movie newMovie) {
        return webClient.post().uri(ADD_MOVIE_V1)
                .syncBody(newMovie)
                .retrieve()
                .bodyToMono(Movie.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClientResponseException - {}", ex.getResponseBodyAsString(), ex);
                    return Mono.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
                .block();
    }

    public Movie updateMovie(Integer movieId, Movie movie) {
        return webClient.put()
                .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                .syncBody(movie)
                .retrieve()
                .bodyToMono(Movie.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClientResponseException - {}", ex.getResponseBodyAsString(), ex);
                    return Mono.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
                .block();
    }

    public String deleteMovieById(Integer movieId) {
        return webClient.delete()
                .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClientResponseException - {}", ex.getResponseBodyAsString(), ex);
                    return Mono.error(new MovieErrorResponse(ex.getStatusText(), ex));
                })
                .block();
    }
}
