package carnival.doc



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.asciidoctor.Asciidoctor.Factory.create
import org.asciidoctor.Asciidoctor



/** */
class Main {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////
    static Logger log = LoggerFactory.getLogger('carnival')


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


