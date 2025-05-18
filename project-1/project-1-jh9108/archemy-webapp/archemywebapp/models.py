from __future__ import unicode_literals

from django.db import models
from django.db.models.signals import post_save
from django.contrib.auth.models import User

# Create your models here.
class ArchemywebappUser(models.Model):
    user = models.OneToOneField(User)
    name = models.CharField(max_length=100)

    def __unicode__(self):
        return self.name

def create_profile_user_callback(sender, instance, **kwargs):
    profile, new = ArchemywebappUser.objects.get_or_create(user=instance)
post_save.connect(create_profile_user_callback, User)
