package carnival.util



/** */
class StringUtils {

    /** 
     * strings_like_this to StringsLikeThis
     * STRINGS_LIKE_THIS to StringsLikeThis
     *
     */
    static public String toCapitalCase(String text) {
        toCamelCase(text, true)
    }

    /** 
     * strings_like_this to stringsLikeThis
     * STRINGS_LIKE_THIS to stringsLikeThis
     *
     */
    static public String toCamelCase(String text, boolean capitalized = false) {
        text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
        return capitalized ? capitalize(text) : text
    }
     
    /** 
     * stringsLikeThis to strings_like_this
     * stringsLikeThis1 to strings_like_this_1
     *
     */
    static public String toSnakeCase(String text) {
        text.replaceAll( /([A-Z0-9])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' )
    }

    /** 
     * stringsLikeThis to STRINGS_LIKE_THIS
     *
     */
    static public String toScreamingSnakeCase(String text) {
    	toSnakeCase(text).toUpperCase()
    }

    /** 
     * stringsLikeThis to strings-like-this
     * stringsLikeThis1 to strings-like-this-1
     *
     */
    static public String toKebabCase(String text) {
        text.replaceAll( /([A-Z0-9])/, /-$1/ ).toLowerCase().replaceAll( /^-/, '' )
    }

    /** */
    static public String toMarkdown(Enum en) {
        def text = en.name()
        text = text.replaceAll('_', ' ')
        text
    }    

    /** */
    static public String escapeUnderscores(String text) {
        text.replaceAll('_', '\\_')
    }

}




