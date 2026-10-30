package br.com.sebratel.massivas.repository.afetados.impl;

/** Titulo de massiva e em quantas massivas distintas ele aparece. */
public interface TituloFrequenteProjection {

    String getTitulo();

    Long getVezes();
}
