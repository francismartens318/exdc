package customconnectornode.discourse.domain

enum DiscourseEntity {
    TOPIC("topic")

    String label

    DiscourseEntity(String label) {
        this.label = label
    }
}