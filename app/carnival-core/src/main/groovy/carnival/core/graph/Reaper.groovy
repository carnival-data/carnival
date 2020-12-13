package carnival.core.graph


trait Reaper {

    public ReaperMethod method(String name) {

    }

}


class ReaperMethod extends GraphMethod { }
class ReaperMethodCall extends GraphMethodCall { }
class ReaperMethodProcess extends GraphMethodProcess { }