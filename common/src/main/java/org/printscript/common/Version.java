package org.printscript.common;

// las versiones del lenguaje que el sistema sabe procesar
// vive acá porque la nombran varios módulos y no cambia nunca; el CONTENIDO de cada
// versión no vive acá, sino en la factory de cada módulo que sabe del lenguaje
public enum Version {
    V10,
    V11
}
