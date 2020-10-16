package carnival.doc



import groovy.util.logging.Slf4j

import static org.asciidoctor.Asciidoctor.Factory.create
import org.asciidoctor.Asciidoctor



/** */
@Slf4j
class Main {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    static void main(String[] args) {
    	log.info "HELLOOOOO!!!!"
    	test()
    }



    static void test() {
		def url = getClass().getResource('/tutorial/test-1.adoc')
		assert url
		def file = new File(url.toURI())
		assert file
		assert file.exists()

		Asciidoctor asciidoctor = create();
		assert asciidoctor

        def text = file.text
        assert text
        println text

		String html = asciidoctor.convert(text, [:])
		println(html)
    }


}


