package nl.surf.filesender.rde.client.activities.enrollment

import nl.surf.filesender.rde.client.activities.general.ReadMRZActivity


class EnrollmentReadMRZActivity : ReadMRZActivity() {

    override var nextActivity: Class<*> = EnrollmentReadNFCActivity::class.java

}